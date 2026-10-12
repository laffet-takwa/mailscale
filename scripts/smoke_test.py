#!/usr/bin/env python3
"""Smoke-test the running MailScale API: health, login, dashboard, template."""

import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("MAILSCALE_API", "http://localhost:8080")
EMAIL = os.environ.get("MAILSCALE_EMAIL", "admin@mailscale.local")
PASSWORD = os.environ.get("MAILSCALE_PASSWORD", "demo1234")


def request(method, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            raw = response.read().decode() or "{}"
            return response.status, json.loads(raw)
    except urllib.error.HTTPError as error:
        return error.code, {"error": error.read().decode()}


def main():
    failures = []

    status, body = request("GET", "/actuator/health")
    print(f"health            : {status}")
    if status != 200:
        failures.append("health")

    status, body = request("POST", "/auth/login", {"email": EMAIL, "password": PASSWORD})
    print(f"login             : {status}")
    if status != 200:
        print(body)
        failures.append("login")
        return report(failures)
    token = body["token"]

    status, dashboard = request("GET", "/analytics/dashboard", token=token)
    print(f"dashboard         : {status} campaigns={dashboard.get('totalCampaigns')}")
    if status != 200:
        failures.append("dashboard")

    status, templates = request("GET", "/templates", token=token)
    print(f"templates         : {status} count={len(templates) if isinstance(templates, list) else 'n/a'}")
    if status != 200 or not templates:
        failures.append("templates")
        return report(failures)

    template_id = templates[0]["id"]
    idempotency_key = f"smoke-{os.getpid()}"
    status, mail = request(
        "POST",
        "/emails",
        {
            "to": "smoke@example.com",
            "templateId": template_id,
            "variables": {"firstName": "Smoke", "company": "Acme"},
            "idempotencyKey": idempotency_key,
        },
        token=token,
    )
    print(f"queue email       : {status} id={mail.get('messageId')}")
    if status != 202:
        failures.append("queue-email")
        return report(failures)

    status, replayed = request(
        "POST",
        "/emails",
        {
            "to": "smoke@example.com",
            "templateId": template_id,
            "variables": {"firstName": "Smoke", "company": "Acme"},
            "idempotencyKey": idempotency_key,
        },
        token=token,
    )
    idempotent = replayed.get("messageId") == mail.get("messageId")
    print(f"idempotent replay : {status} same_message_id={idempotent}")
    if status != 202 or not idempotent:
        failures.append("idempotency")

    return report(failures)


def report(failures):
    if failures:
        print(f"\nFAILED: {', '.join(failures)}")
        return 1
    print("\nAll smoke checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
