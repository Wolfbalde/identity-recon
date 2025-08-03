# identity-recon
This project implements a REST API for customer identity reconciliation to track loyal customers across multiple orders with varying contact information. It demonstrates a robust approach to deduplicating and linking customer identities based on shared email or phone numbers, consolidating all related records with proper "primary" and "secondary" contact handling.

Features:

RESTful endpoint for resolving and merging customer identities.

Persistent storage using Spring Data JPA and a PostgreSQL database.

Handles creation of new "primary" contacts and dynamic linking of "secondary" contacts.

Supports merging of disparate contact chains if they are subsequently identified as the same user.

Fully compliant with Bitespeed’s described logic and output format.

Tech Stack Used:

Language: Java

Framework: Spring Boot

ORM: Spring Data JPA/Hibernate

Database: PostgreSQL (configurable)

Optional: Lombok for reduced boilerplate

Usage:

Clone the repository and run with your favorite IDE or with Maven/Gradle.

Configure your DB connection in application.properties.

Hit the /identify endpoint with below JSON payload:

{
"email": "emailid",
"phoneNumber": "phonenum"
}
