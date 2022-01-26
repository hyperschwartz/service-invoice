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

To communicate with onboarding api, you have two options:
1. Simulate the service (before running the app, enable simulation in the terminal):
```shell
export USE_SIMULATED_ASSET_ONBOARDING=true
```
2. Add an api key to requests (before running the app, export the api key in the terminal):
```shell
export ONBOARDING_API_KEY=ask-develops-for-this-value-if-you-need-it
```

### Deployment
Maybe someday....
