# Logger

This library facilitates tracing different types of information, including technical logs, audit trail, statistics, ...

The purpose of the audit trail logs is to have a support for day to day business.

They contain functional log of each exchanged message by a component.

JSON formatting is used. It allows easy and no breaking changes.

Each instance of Gate (Micro-services) logs data in dedicated files.

These files can then be collected and gathered into a logs sink. These tasks are out of scope of reference implementation
