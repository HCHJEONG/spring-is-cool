# Manual Deployment Notes

These scripts intentionally keep local development, local Docker image creation,
and remote deployment as separate steps.

The Docker image is never built from this working tree. Each deploy script
updates a clean local clone under:

```text
~/hchjeong/deploy_remote_repo/spring-is-cool
```

Then it builds a Docker image from that clone, exports it as a `.tar`, transfers
the tarball to the target host, loads it there, and replaces only the
application container.

Runtime SSH credentials, host binding, ports, and host key path are supplied at
`docker run` time. Do not commit env files, SSH host keys, private keys, or image
archives.

The application SSH service must be reachable through two layers:

1. Inside Docker, Spring must bind to `0.0.0.0`.
2. Outside Docker, the host firewall, LAN route, or AWS Security Group must
   allow the chosen host port.

The deploy scripts publish `0.0.0.0:${HOST_PORT}:2222` and set:

```text
SPRING_IS_COOL_SSH_HOST=0.0.0.0
SPRING_IS_COOL_SSH_PORT=2222
```

## Dev Demo

Default target:

```text
yoga
```

Run from WSL:

```bash
.fordeploy/deploy-dev-demo.sh
```

If the dev-demo host firewall blocks inbound app SSH traffic, open the selected
host port on that machine. For example:

```bash
ssh yoga 'sudo ufw allow 2222/tcp'
```

After deployment:

```bash
ssh demo@192.168.0.104 -p 2222
```

## AWS Demo

The AWS target is deliberately explicit. Provide `AWS_DEMO_HOST` when running:

```bash
AWS_DEMO_HOST=ubuntu@example .fordeploy/deploy-aws-demo.sh
```

The AWS instance Security Group must allow inbound TCP traffic to `HOST_PORT`
from the desired client CIDR. Keep that infrastructure change explicit and
outside the deploy script. In this environment, make that change manually from
the AWS bastion host using the AWS CLI available there.

If `aws-demo` later needs a bastion/private-host path, keep that in the
aws-specific script rather than sharing target details with `dev-demo`.
