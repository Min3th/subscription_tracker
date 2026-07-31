# Subtrak AWS SES infrastructure

`ses-email.yaml` provisions the SES migration resources in one CloudFormation
stack. It can optionally create a Route 53 zone for the inbound subdomain, but
delegation at the parent DNS provider remains a controlled deployment step. It
does not enable the application's SES feature flags.

## Before deployment

Choose an SES region that supports email receiving. Deploy the stack in that
same region. The `SesRegion` parameter must equal the CloudFormation deployment
region.

Collect:

- the AWS account ID;
- the existing EC2 instance-role name (not its ARN or instance-profile name);
- the sending domain;
- the inbound subdomain, normally `inbound`;
- the database content-retention period plus one day, normally `31`.

The deploying principal needs permission to create SES, S3, SNS, SQS, Route 53,
IAM policy, and CloudFormation resources. Because the stack attaches a policy
to an existing role, deployment requires `CAPABILITY_NAMED_IAM`.

## Validate

```bash
aws cloudformation validate-template \
  --region ap-south-1 \
  --template-body file://infrastructure/ses-email.yaml
```

## Deploy without changing DNS

```bash
aws cloudformation deploy \
  --region ap-south-1 \
  --stack-name subtrak-production-email \
  --template-file infrastructure/ses-email.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    EnvironmentName=production \
    DomainName=subtrak.xyz \
    InboundSubdomain=inbound \
    AwsAccountId=123456789012 \
    SesRegion=ap-south-1 \
    Ec2InstanceRoleName=replace-with-existing-role-name \
    RawMimeRetentionDays=31 \
    CreateInboundHostedZone=false
```

Do not publish the MX record during this initial deployment. Existing SendGrid
inbound delivery remains active.

Before the inbound cutover, confirm in the Amazon SES console that
`subtrak-production-inbound` is the active receipt rule set. If it is not active,
select it under **Email receiving -> Rule sets** and choose **Set as active**.
Only one receipt rule set can be active in an AWS Region.

## Stack outputs

Publish the three DKIM CNAME name/value pairs from the stack outputs. After SES
reports the identity as verified, request production sending access for the
selected region.

Use these outputs for the application:

| Output                | Environment variable    |
| --------------------- | ----------------------- |
| `SesRegion`           | `SES_REGION`            |
| `SesConfigurationSet` | `SES_CONFIGURATION_SET` |
| `SesInboundQueueUrl`  | `SES_INBOUND_QUEUE_URL` |
| `SesEventQueueUrl`    | `SES_EVENT_QUEUE_URL`   |
| `SesInboundBucket`    | `SES_INBOUND_BUCKET`    |

Configure `SES_FROM_EMAIL` and `SES_FROM_NAME` separately. Keep
`EMAIL_OUTBOUND_PROVIDER=sendgrid` and `SES_CONSUMERS_ENABLED=false` until the
staged rollout begins.

The `InboundMxRecordName` and `InboundMxRecordValue` outputs are for the later
controlled inbound cutover. Publishing that MX record routes new mail to SES.

## Delegated inbound DNS

When the parent DNS provider cannot add an MX record without replacing
root-domain mail settings, update the stack with:

```bash
CreateInboundHostedZone=true
```

This creates a public Route 53 zone for `inbound.<domain>` and its SES MX
record. A public hosted zone currently costs USD 0.50 per month plus DNS query
charges. Add every name server from `InboundDelegationNameServers` as an `NS`
record with host `inbound` at the parent DNS provider. Do not replace the
parent domain's name servers or root MX records.

Before delegation propagates, test directly against one output name server:

```powershell
.\scripts\Test-SesDnsCutover.ps1 -NameServer <one-route53-name-server>
```

After delegation, run the check without `-NameServer`.

## Read-only readiness check

After authenticating the AWS CLI, run:

```powershell
.\scripts\Test-SesReadiness.ps1
.\scripts\Test-SesReadiness.ps1 -RequireCutoverReady
```

Strict mode requires SES production access, account sending, the active receipt
rule set, and empty DLQs. Source-queue backlog is reported without reading
message bodies.

## Retained resources

The raw MIME bucket has both `DeletionPolicy: Retain` and
`UpdateReplacePolicy: Retain`. Deleting the stack therefore does not delete raw
email objects. S3 lifecycle rules still expire current objects after
`RawMimeRetentionDays` and noncurrent versions after one day.
