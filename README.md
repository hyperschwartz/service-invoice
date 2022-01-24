# Service Invoice
Handles Invoice generation

## Running

### Local
The application depends on a dedicated database.  To spin up the local database, just run from the root directory:
```shell
./localnet-up.sh
```

To check that the database is available, just run the following command and look for a container called
local-development-postgres.
```shell
docker ps
```

After verifying your database is up, just boot the app.  It runs on port 13459.
```shell
./gradlew bootRun
```

### Deployment
Maybe someday....
