package ru.javaprac.bank.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Convert(converter = ClientTypeConverter.class)
    @Column(name = "type", nullable = false)
    private ClientType type;

    @Column(name = "surname")
    private String surname;

    @Column(name = "second_name")
    private String secondName;

    @Column(name = "passport", unique = true)
    private String passport;

    @Column(name = "snils", unique = true)
    private String snils;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "link_to_photo")
    private String linkToPhoto;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Account> accounts = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public ClientType getType() { return type; }
    public void setType(ClientType type) { this.type = type; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getSecondName() { return secondName; }
    public void setSecondName(String secondName) { this.secondName = secondName; }
    public String getPassport() { return passport; }
    public void setPassport(String passport) { this.passport = passport; }
    public String getSnils() { return snils; }
    public void setSnils(String snils) { this.snils = snils; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLinkToPhoto() { return linkToPhoto; }
    public void setLinkToPhoto(String linkToPhoto) { this.linkToPhoto = linkToPhoto; }
    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }
}
