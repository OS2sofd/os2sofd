# OS2sofd

OS2sofd is the authoritative source of organisation and identity data for Danish
municipalities. It collects persons, organisational units, affiliations and user
accounts from source systems such as payroll/HR, Active Directory and CPR,
enriches the data, and distributes it to other systems. It can also drive
automated user account management (creation, maintenance and deprovisioning).

This repository contains the server application. OS2sofd is part of
[OS2](https://www.os2.eu/), the Danish public sector open source community. The
product page is at [os2.eu/os2sofd](https://www.os2.eu/os2sofd).

## Documentation

All documentation is published at [www.sofd.io](https://www.sofd.io/) (in Danish).

- [Brugervejledning](https://www.sofd.io/brugervejledning/), the end user guide
- [Teknik](https://www.sofd.io/teknik/), architecture, login and SAML, API and batch jobs
- [Integrationer](https://www.sofd.io/integrationer/) and [Agents](https://www.sofd.io/agents/), the on-premise agents and system integrations
- [Implementeringsdrejebøger](https://www.sofd.io/implementation-guides/), how to get started as a new municipality
- [Changelog](https://www.sofd.io/changelog/), release notes per version

Please do not add documentation to this repository. It belongs in the documentation site.

## Support, bugs and change requests

- Bugs and change requests are filed at [github.com/OS2sofd/issues](https://github.com/OS2sofd/issues/issues/new?template=change_request.yml)
- Planning and status can be followed on the [OS2sofd project board](https://github.com/orgs/OS2sofd/projects/1)
- Operational status is published at [driftstatus.digital-identity.dk](https://driftstatus.digital-identity.dk/product/1)
- Security vulnerabilities must be reported privately, see [SECURITY.md](SECURITY.md)

## Building

Requirements are Java 25, Maven 3 and a MySQL 8 database.

```bash
mvn clean install
```

The executable JAR is produced at `ui/target/sofd-core.jar`. Add `-DskipTests` to
skip the test suite.

## Running locally

The application expects a MySQL database named `sofd` on `localhost:3306`. Flyway
creates and migrates the schema on startup. Local settings are read from
`ui/config/application.properties`.

```bash
cd ui
mvn spring-boot:run
```

## Repository layout

- `ui/` is the Spring Boot web application (REST API, web UI, services, JPA entities, scheduled tasks, Flyway migrations)
- `webjar/` bundles the frontend libraries used by the web UI
- `doc/` holds integration reference files and scripts, not documentation

## Technical notes

### MySQL GROUP_CONCAT limit

When many KLE codes are in use on organisational units or affiliations, the
`GROUP_CONCAT` result can exceed the MySQL default of 1024 bytes, which silently
truncates the data. Increase the limit in the MySQL configuration.

```
group_concat_max_len = 10240
```

## License

OS2sofd is licensed under the [Mozilla Public License 2.0](LICENSE).
