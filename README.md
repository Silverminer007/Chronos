# Chronos

A PWA (Progressive-Web-Application) to manage dates of teams. This is especially made for teams of volunteers, that do not have fixed working hours or contracts, but work on a best effort basis. This application allows to visualise whom is able to attend a meeting and whom is not going to come. It also offers custoizable date reminders via E-Mail and Signal (Experimental). Administrators can also create polls for all team members that remind them to give feedback for a date.

## Installation

This application is fully dockerized. You can install and use it in every docker compatible environment. Even Kubernetes is supported. To get started, take a look at 'docker-compose.yaml' and copy it to the machine you want to deploy this to. Please also copy '.env.example', change the Environment variables to your needs and rename the file to '.env'.

If docker is already installed on your machine, you can start the application by running 'docker compose up -d' / 'docker-compose up -d' / 'sudo docker compose up -d' depending on your installation.

To make the application reachable through the internet, you habe to create a reverse proxy, that manages SSL/TLS encryption and forwards requests to the application.

## Usage

In the future, you'll also be able to use the hosted version of chronos on https://chronos-live.de.
That's not yet possible
