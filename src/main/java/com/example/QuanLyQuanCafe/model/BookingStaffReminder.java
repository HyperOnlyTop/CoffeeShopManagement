package com.example.QuanLyQuanCafe.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "booking_staff_reminders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"table_booking_id", "kind"})
)
public class BookingStaffReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "table_booking_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TableBooking tableBooking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BookingReminderKind kind;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TableBooking getTableBooking() {
        return tableBooking;
    }

    public void setTableBooking(TableBooking tableBooking) {
        this.tableBooking = tableBooking;
    }

    public BookingReminderKind getKind() {
        return kind;
    }

    public void setKind(BookingReminderKind kind) {
        this.kind = kind;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
