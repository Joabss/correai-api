package com.correai.api.adapter.out.persistence.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_state")
class AdminStateEntity {
    @Id
    int id;
    @Column(nullable = false)
    boolean initialized;

    protected AdminStateEntity() {}
}
