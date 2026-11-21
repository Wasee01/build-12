package com.example.auditing.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationFailureRepository extends JpaRepository<NotificationFailure, Long> { }
