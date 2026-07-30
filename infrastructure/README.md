3# Subtrak AWS SES infrastructure

`ses-email.yaml` provisions the SES migration resources in one CloudFormation
stack. It does not change DNS and does not enable the application's SES feature
flags.

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

The deploying principal needs permission to create SES, S3, SNS, SQS, IAM role,
IAM policy, and CloudFormation resources. Because the stack attaches a policy to
an existing role, deployment requires `CAPABILITY_NAMED_IAM`.

## Validate

```bash
aws cloudformation validate-template \
  --region ap-southeast-1 \
  --template-body file://infrastructure/ses-email.yaml
```

## Deploy without changing DNS

```bash
aws cloudformation deploy \
  --region ap-southeast-1 \
  --stack-name subtrak-production-email \
  --template-file infrastructure/ses-email.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    EnvironmentName=production \
    DomainName=subtrak.me \
    InboundSubdomain=inbound \
    AwsAccountId=123456789012 \
    SesRegion=ap-southeast-1 \
    Ec2InstanceRoleName=replace-with-existing-role-name \
    RawMimeRetentionDays=31
```

Do not publish the MX record during this initial deployment. Existing SendGrid
inbound delivery remains active.

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

## Retained resources

The raw MIME bucket has both `DeletionPolicy: Retain` and
`UpdateReplacePolicy: Retain`. Deleting the stack therefore does not delete raw
email objects. S3 lifecycle rules still expire current objects after
`RawMimeRetentionDays` and noncurrent versions after one day.
