# Getting Started

### Reference Documentation

* [Official Maven documentation](https://maven.apache.org/guides/)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/maven-plugin)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)

### Connecting to PostgreSQL on EC2

Keep an SSH local port-forward open in a terminal on your computer:

```sh
ssh -i <path-to-key.pem> -L 5432:127.0.0.1:5432 ec2-user@<instance-public-dns>
```

Set these environment variables in the environment that starts the API. For
PowerShell, for example:

```powershell
$env:DB_NAME = "<database-name>"
$env:DB_USERNAME = "<database-user>"
$env:DB_PASSWORD = "<database-password>"
mvn spring-boot:run
```

The API connects to `127.0.0.1:5432` by default. Set `DB_HOST` and `DB_PORT` if
the local tunnel uses a different address or port. For an IDE run configuration,
set the same variables there. Do not commit database credentials.

On startup, Spring Data JPA creates or updates the `users` table with `id`,
`first_name`, `last_name`, `email_address`, `created_date`, and `updated_date`
columns. The ID is generated starting at 1, and the timestamp fields are set
when a user is created or updated.

### User API

The API provides these CRUD endpoints:

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get one user |
| `POST` | `/api/users` | Create a user; returns `201 Created` and a `Location` header |
| `PUT` | `/api/users/{id}` | Replace a user's first name, last name, and email address |
| `DELETE` | `/api/users/{id}` | Delete a user; returns `204 No Content` |

The create and update request body is JSON with `firstName`, `lastName`, and
`emailAddress` fields. Requests for a missing user return `404 Not Found`.

Run the tests with `mvn test`, or package the application with `mvn package`.

### Deploy from GitHub Actions

The `.github/workflows/deploy-ec2.yml` workflow runs on pushes to `main` and can
also be started manually. It runs `mvn clean verify`, uploads the JAR to a
private S3 bucket, and uses Systems Manager Run Command to install it and restart
the `aws-sandbox-api` systemd service on the EC2 instance.

Before enabling the workflow:

* Register the EC2 instance as an online Systems Manager managed node. Attach an
  instance profile with `AmazonSSMManagedInstanceCore` and narrowly scoped
  `s3:GetObject` permission for the deployment JAR.
* Create a private S3 bucket with Block Public Access enabled.
* Configure GitHub OIDC in AWS and create a role trusted only for this
  repository's `main` branch. Limit its permissions to uploading the deployment
  object, deleting the staged object, and sending/polling Run Command for this
  instance.
* Add these GitHub Actions repository variables: `AWS_REGION`,
  `AWS_ROLE_ARN`, `EC2_INSTANCE_ID`, and `DEPLOY_BUCKET`.

No SSH private key or long-lived AWS access key is needed in GitHub. Keep
PostgreSQL credentials in `/etc/aws-sandbox-api.env` on the instance; the
workflow does not upload or change that file. The workflow deletes the staged
JAR after deployment, including failed deployment attempts. Keep S3 bucket
versioning disabled or configure noncurrent-version expiration, because deletes
in a versioned bucket leave prior object versions retained.
