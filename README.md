
# dms-submission-admin-frontend

This is a service which provides admin functionality for consumers of the [dms-submission service](https://github.com/hmrc/dms-submission)

It can be used for both local development and in deployed environments.

It requires internal auth to access.

## Features

- Has a list of services that your UMP user has access to which have submitted to `dms-submission` recently
- Has a breakdown of all submissions for a particular service, by day and status
- Has a list of submissions by day
- Has a page for individual submissions which shows information such as the status and SDES correlation ID used
- Allows users to manually retry failed submissions

> Note: This service specifically does not give the user access to the actual files submitted to the service,
> only the metadata associated with the submissions themselves.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").