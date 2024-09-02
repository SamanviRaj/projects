package com.eqh.party.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "E_MAIL_ADDRESS")
@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
@NoArgsConstructor // Lombok annotation to generate a no-args constructor
@AllArgsConstructor // Lombok annotation to generate a constructor with all fields
@Builder // Lombok annotation to provide a builder pattern
public class EmailAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Using identity strategy for auto-increment
    private Long id;

    @Column(name = "addr_line", nullable = false, length = 320)
    private String addrLine;

    @Column(name = "attachment_ind", nullable = false)
    private Boolean attachmentInd;

    @Column(name = "email_type", nullable = false, length = 10)
    private String emailType;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "invalidemail_start_date")
    private LocalDate invalidemailStartDate;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "prefemail_addr", nullable = false)
    private Boolean prefemailAddr;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "undeliverable_ind", nullable = false)
    private Boolean undeliverableInd;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;
}

