package com.example.services;

import com.example.dto.*;
import com.example.exceptions.NoRolesException;
import com.example.exceptions.SecurityExceptionResponse;
import com.example.exceptions.TakenUserNameException;
import com.example.model.Employee;
import com.example.model.Roles;
import com.example.repository.EmployeeRepository;
import com.example.repository.RolesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@AllArgsConstructor
public class UserManagementService {

    @Value("${url.security.login}")
    String securityLoginUrl;
    @Value("${url.security.userManagement}")
    String securityUserManagementUrl;

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RolesRepository rolesRepository;

    //--------------------------------------------------

    /**
     Registers new employees based on the provided authorization header and a list of registration request data.
     @param authorizationHeader The authorization header containing the authentication token.
     @param registerRequestDtoList A list of RegisterRequestDto objects containing the registration data for each employee.
     @return A Set<Employee> containing the newly registered employees.
     @throws IllegalStateException if the provided username already exists for any employee in the database.
     */
    public Set<Employee> registerEmployee(String authorizationHeader, List<RegisterRequestDto> registerRequestDtoList) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("Authorization", authorizationHeader);
        HttpEntity<JwtGenerationRequestDto> requestEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<Object> validationResponse = restTemplate.exchange(securityUserManagementUrl, HttpMethod.POST, requestEntity, Object.class);

        if(validationResponse.getStatusCode().isError()) {
            throw new SecurityException(validationResponse.getBody().toString(), validationResponse.getStatusCode());
        }

        Set<Employee> employeeSet = new HashSet<>();

        for(RegisterRequestDto registerRequestDto: registerRequestDtoList) {

            Optional<Employee> employeeControl = employeeRepository.findByUsername(registerRequestDto.getUsername());

            // check if the user with that username exists
            if(employeeControl.isPresent()) {
                throw new TakenUserNameException("Username: " + employeeControl.get().getUsername() + " is taken");
            }

            // if not create an employee
            String  username = registerRequestDto.getUsername();

            if(registerRequestDto.getRoleSet().size() == 0) {
                throw new NoRolesException("An employee mush have at least one role");
            }

            Employee employee = new Employee();
            employee.setUsername(username);
            employee.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
            employee.setEmail(registerRequestDto.getEmail());
            employee.setRoles(getRolesSetFromRegisterRoleDtoSet(employee, registerRequestDto.getRoleSet()));

            employeeSet.add(employeeRepository.save(employee));
        }
        return employeeSet;
    }

    public Set<Employee> registerAdmin(List<RegisterRequestDto> registerRequestDtoList) {

        Set<Employee> employeeSet = new HashSet<>();

        for(RegisterRequestDto registerRequestDto: registerRequestDtoList) {

            Optional<Employee> employeeControl = employeeRepository.findByUsername(registerRequestDto.getUsername());

            // check if the user with that username exists
            if(employeeControl.isPresent()) {
                throw new TakenUserNameException("Username: " + employeeControl.get().getUsername() + " is taken");
            }
            // check if the register request contains at least one role
            if(registerRequestDto.getRoleSet().size() == 0) {
                throw new NoRolesException("An employee mush have at least one role");
            }

            // if not create an employee
            String  username = registerRequestDto.getUsername();
            Employee employee = new Employee();
            employee.setUsername(username);
            employee.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
            employee.setEmail(registerRequestDto.getEmail());
            employee.setRoles(getRolesSetFromRegisterRoleDtoSet(employee, registerRequestDto.getRoleSet()));

            employeeSet.add(employeeRepository.save(employee));
        }
        return employeeSet;
    }

    /**
     Performs a login request with the provided login credentials and retrieves a JWT token.
     @param loginRequestDto The LoginRequestDto object containing the login credentials.
     @return A JwtDto object containing the JWT token retrieved from the login response.
     */
    public JwtDto login(LoginRequestDto loginRequestDto) {
        Employee employeee = employeeRepository.findById(52L).get();

        HttpEntity<LoginRequestDto> requestEntity = new HttpEntity<>(loginRequestDto); // first parameter is the body
        ResponseEntity<JwtDto> jwtResponse = restTemplate.exchange(securityLoginUrl, HttpMethod.POST, requestEntity, JwtDto.class);

        JwtDto jwtDto = jwtResponse.getBody();

        return jwtDto;
    }

    /**
     Deletes an employee by their ID, authenticated with the provided authorization header.
     @param authorizationHeader The authorization header containing the authentication token.
     @param id The ID of the employee to be deleted.
     @return The ID of the deleted employee.
     @throws IllegalStateException if no employee exists with the provided ID.
     */
    @Transactional
    public Long deleteEmployeeById(String authorizationHeader, Long id) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("Authorization", authorizationHeader);
        HttpEntity<JwtGenerationRequestDto> requestEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<Object> validationResponse = restTemplate.exchange(securityUserManagementUrl, HttpMethod.POST, requestEntity, Object.class);

        if(!employeeRepository.existsById(id)) throw new IllegalStateException("Employee with id " + id + " does not exists");

        else {
            employeeRepository.setDeletedTrue(id);
            return id;
        }
    }

    /**
     Updates an employee with the provided authorization header, employee ID, and update request data.
     @param authorizationHeader The authorization header containing the authentication token.
     @param id The ID of the employee to be updated.
     @param updateRequestDto The UpdateRequestDto object containing the updated employee data.
     @return The updated Employee object.
     @throws IllegalStateException if no employee exists with the provided ID.
     */
    @Transactional
    public Employee updateEmployee(String authorizationHeader, Long id, UpdateRequestDto updateRequestDto) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("Authorization", authorizationHeader);
        HttpEntity<JwtGenerationRequestDto> requestEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<Object> validationResponse = restTemplate.exchange(securityUserManagementUrl, HttpMethod.POST, requestEntity, Object.class);

        if(!employeeRepository.existsById(id)) throw new IllegalStateException("Employee with id " + id + " does not exists"); // string builder

        String username = updateRequestDto.getUsername();

        String password = null;
        if(updateRequestDto.getPassword() != null) {
             //password = passwordEncoder.encode(updateRequestDto.getPassword());
             password = updateRequestDto.getPassword();
        }

        String email = updateRequestDto.getEmail();

        employeeRepository.updateEmployeeById(id, username, password, email);


        Employee employee = employeeRepository.findById(id).get();

        Set<Roles> rolesSet = getRolesSetFromUpdateRoleDtoSet(employee, updateRequestDto.getRoleSet());

        if(rolesSet.size() != 0) {
            rolesRepository.deleteRoleById(employee.getId());
            //employee.getRoles().clear();
            employee.updateRoles(rolesSet);
        }
        employeeRepository.save(employee);

        return employee;
    }

    //-----------------------------------------------------

    private ResponseEntity<Object> sendResponseEntity(ResponseEntity<Object> responseEntity) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SecurityExceptionResponse securityExceptionResponse = objectMapper.readValue(responseEntity.getBody().toString(), SecurityExceptionResponse.class);
        throw new SecurityException(securityExceptionResponse.getMessage());
    }

    /**
     Converts a Set of RegisterRequestDto.RoleDto objects into a Set of Roles objects associated with the provided employee.
     @param employee The Employee object to associate with the roles.
     @param roleDtoSet A Set of RegisterRequestDto.RoleDto objects containing role data.
     @return A Set of Roles objects associated with the provided employee and extracted from the roleDtoSet.
     */
    public Set<Roles> getRolesSetFromRegisterRoleDtoSet(Employee employee, Set<RegisterRequestDto.RoleDto> roleDtoSet) {
        Set<Roles> newRolesSet = new HashSet<>();

        for (RegisterRequestDto.RoleDto roleDto : roleDtoSet) {

            Roles role = new Roles(employee, roleDto.getRoleName());
            //Roles role = Roles.builder().employee(employee).roleName(roleDto.getRoleName()).build();
            newRolesSet.add(role);
        }
        return newRolesSet;
    }

    private Set<Roles> getRolesSetFromUpdateRoleDtoSet(Employee employee, Set<UpdateRequestDto.RoleDto> roleDtoSet) {
        Set<Roles> newRolesSet = new HashSet<>();

        for (UpdateRequestDto.RoleDto roleDto : roleDtoSet) {
            Roles role = new Roles(employee, roleDto.getRoleName());
            newRolesSet.add(role);
        }
        return newRolesSet;
    }

    private void getRolesSetRoleFromDtoSet2(Long id, Set<UpdateRequestDto.RoleDto> roleDtoSet) {

        Employee employee = employeeRepository.findById(id).orElseThrow();
        employee.getRoles().clear();
        employeeRepository.save(employee);
    }
}
