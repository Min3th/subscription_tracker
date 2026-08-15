resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-database"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.name_prefix}-database-subnets"
  }
}

resource "aws_security_group" "this" {
  name_prefix = "${var.name_prefix}-database-"
  description = "Controls access to the SubTrak PostgreSQL database"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name_prefix}-database-sg"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "postgresql" {
  for_each = var.allowed_security_group_ids

  security_group_id            = aws_security_group.this.id
  referenced_security_group_id = each.value
  description                  = "PostgreSQL access from an authorized application security group"

  # Allow only 5432 (this doesn't mean source and destination)

  from_port   = 5432
  to_port     = 5432
  ip_protocol = "tcp"

}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-postgresql"

  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  db_name  = var.database_name
  username = var.master_username

  manage_master_user_password = true

  port = 5432

  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.this.id]
  publicly_accessible    = false

  multi_az                = var.multi_az
  backup_retention_period = var.backup_retention_days

  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true
  apply_immediately          = var.apply_immediately

  deletion_protection = var.deletion_protection
  skip_final_snapshot = var.skip_final_snapshot

  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.name_prefix}-postgresql-final"

  performance_insights_enabled = false
  monitoring_interval          = 0

  tags = {
    Name = "${var.name_prefix}-postgresql"
  }
}
