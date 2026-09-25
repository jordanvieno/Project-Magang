# Project Overview

Welcome to the project repository! This repository contains the source code, CI/CD pipeline definitions, and Kubernetes deployment configurations for our enterprise application.

## Repository Structure

- `src/` - Core Java source code for the application.
- `digital-channel-app/` - Application module/components.
- `k8s/` - Kubernetes deployment manifests.
- `Dockerfile` - Instructions for containerizing the application.
- `Jenkinsfile` - CI/CD pipeline configuration.
- `pom.xml` - Maven project configuration and dependencies.

## Branching Strategy

This project utilizes the following primary branches for development and deployment:

*   **`main`**: The production-ready branch. Code in this branch represents the stable release and is what gets deployed to the production environment.
*   **`development`**: The active development branch. All new features, bug fixes, and updates are integrated into this branch for testing and validation before being promoted to `main`.

*(Note: Please ensure all pull requests are directed to the appropriate branch.)*

## Overview of Technologies

- **Java & Maven**: Core application development and build tool.
- **Docker**: Containerization of the application.
- **Kubernetes**: Container orchestration for deployment.
- **Jenkins**: Automated CI/CD pipelines.

## Arsitektur & CI/CD Pipeline

Berikut adalah gambaran alur arsitektur deployment beserta CI/CD pipeline yang diatur melalui `Jenkinsfile`:

```mermaid
graph TD
    A[Developer Commits Code] -->|Push to GitHub| B(Jenkins SCM Poll)
    B --> C{Branch?}
    C -->|development| D[Security Code Scan / SAST]
    C -->|main| D
    D --> E[Build & Unit Test]
    
    subgraph Build Phase
        E --> F[Backend: Maven Build]
        E --> G[Frontend: Node.js/NPM Build]
    end
    
    F --> H[Containerization: Docker Build]
    G --> H
    
    H --> I[Deploy to SIT Server]
    I --> J[Automated Integration & API Test]
    
    J --> K{Is Branch main?}
    K -->|No| L([Finish: Deployed to SIT])
    K -->|Yes| M[Approval for Production]
    
    M -->|Approved| N[Deploy to Production]
    
    subgraph Production Deployment
        N --> O[Backend: Kubernetes Cluster]
        N --> P[Frontend: Nginx Web Server Docker]
    end
    
    O --> Q[Observability: Splunk / ELK Monitoring]
    P --> Q
    Q --> R([Finish: Production Release])
```
