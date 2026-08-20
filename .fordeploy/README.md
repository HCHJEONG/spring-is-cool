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

The dev demo keeps repository-specific runtime files under:

```text
/home/hchjeong/spring-is-cool
/home/hchjeong/docker_images/spring-is-cool
```

Runtime files that must stay outside the Docker image live directly under the
application runtime directory:

```text
/home/hchjeong/spring-is-cool/.env.local
/home/hchjeong/spring-is-cool/gcp-key.json
/home/hchjeong/spring-is-cool/runtime/ssh/hostkey.ser
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

The AWS target uses the local `aws-demo` SSH alias by default. That alias reaches
the private instance through `aws-bastion`.

The AWS demo keeps repository-specific runtime files under the same home-based
layout used by the other demo apps:

```text
/home/ubuntu/spring-is-cool
/home/ubuntu/docker_images/spring-is-cool
```

Runtime files that must stay outside the Docker image live directly under the
application runtime directory:

```text
/home/ubuntu/spring-is-cool/.env.local
/home/ubuntu/spring-is-cool/gcp-key.json
/home/ubuntu/spring-is-cool/runtime/ssh/hostkey.ser
```

Older deployments used `/srv/spring-is-cool`, but that legacy runtime directory
has been retired. New deployments should use the home-based paths above.

```bash
.fordeploy/deploy-aws-demo.sh
```

The AWS instance Security Group must allow inbound TCP traffic to `HOST_PORT`
from the desired client CIDR. Keep that infrastructure change explicit and
outside the deploy script. In this environment, make that change manually from
the AWS bastion host using the AWS CLI available there.

Current `aws-demo` is a private instance without a public IP. Security Group
rules alone cannot make it directly reachable from the internet. After
deployment, connect through the bastion:

```bash
ssh -J aws-bastion demo@172.31.76.194 -p 2222
```

If `aws-demo` later needs direct public access, use an explicit AWS entrypoint
such as an Elastic IP or a TCP load balancer, and narrow the Security Group
before exposing it.
