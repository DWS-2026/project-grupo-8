package com.hashpass.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String name; // Gratuito, Premium, Platinum
    private double price;

    @OneToMany(mappedBy = "plan")
    private List<User> users; // Un plan puede tener muchos usuarios

    public Plan() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

}