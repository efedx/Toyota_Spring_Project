package com.example.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
public class Roles {

    @Id
    @SequenceGenerator(name = "seq_gen", sequenceName = "seq", initialValue = 1)
    @GeneratedValue(generator = "seq_gen", strategy = GenerationType.SEQUENCE)
    private Long roleId;


//    public enum RoleNameEnum {
//        ADMIN,
//        OPERATOR,
//        LEADER
//    }

//    @Column(name = "roleName", unique = true, nullable = false)
//    @Enumerated(value = EnumType.STRING)
//    private RoleEnum roleName;

//    @Enumerated(EnumType.STRING)
//    private RoleNameEnum roleNameEnum;

    private String roleName;


//    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "roles") // consider cascade type
//    @NotEmpty
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    //@ElementCollection
    @JsonBackReference("employee_roles")
    private Employee employee;

//    @JsonIgnore
//    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, targetEntity = Employee.class)
//    @JoinColumn(name = "customerId")
//    private Set<Employee> employees = new HashSet<>();

    public Roles(Employee employee, String roleName) {
        this.employee = employee;
        this.roleName = roleName;
    }
    public Roles(String roleName) {
        this.roleName= roleName;
    }
}
