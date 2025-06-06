provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

locals {
  app_full_name = "${var.product}-${var.component}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source                  = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product                 = var.product
  env                     = var.env
  tenant_id               = var.tenant_id
  object_id               = var.jenkins_AAD_objectId
  resource_group_name     = azurerm_resource_group.rg.name
  product_group_name      = "dcd_ccd"
  common_tags             = var.common_tags
  create_managed_identity = true
  additional_managed_identities_access = var.additional_managed_identities_access
}

resource "azurerm_key_vault_secret" "AZURE_APPINSIGHTS_KEY" {
  name         = "AppInsightsInstrumentationKey"
  value        = module.application_insights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
}

module "application_insights" {
  source = "git@github.com:hmcts/terraform-module-application-insights?ref=4.x"

  env     = var.env
  product = var.product
  name    = var.product

  resource_group_name = azurerm_resource_group.rg.name

  common_tags = var.common_tags
}

moved {
  from = azurerm_application_insights.appinsights
  to   = module.application_insights.azurerm_application_insights.this
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "ts_translation_service_s2s_key" {
  name         = "microservicekey-ts-translation-service"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "ts_translation_service_s2s_secret" {
  name         = "ts-translation-service-s2s-secret"
  value        = data.azurerm_key_vault_secret.ts_translation_service_s2s_key.value
  key_vault_id = module.key-vault.key_vault_id
}

////////////////////////////////
// Populate Vault with DB info /
////////////////////////////////

module "postgresql_v15" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  admin_user_object_id = var.jenkins_AAD_objectId
  business_area        = "cft"
  common_tags          = var.common_tags
  component            = var.component
  env                  = var.env
  pgsql_databases = [
    {
      name = var.database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements,pg_buffercache,hypopg"
    }
  ]
  pgsql_version    = "15"
  product          = var.product
  name             = "${local.app_full_name}-postgres-db-v15"
  pgsql_sku        = var.pgsql_sku
  pgsql_storage_mb = var.pgsql_storage_mb
  # Setup Access Reader db user
  force_user_permissions_trigger = "1"

  # Sets correct DB owner after migration to fix permissions
  enable_schema_ownership        = var.enable_schema_ownership
  force_schema_ownership_trigger = "1"
  kv_subscription                = var.kv_subscription
  kv_name                        = module.key-vault.key_vault_id
  user_secret_name               = azurerm_key_vault_secret.POSTGRES-USER.name
  pass_secret_name               = azurerm_key_vault_secret.POSTGRES-PASS.name
}
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.postgresql_v15.username
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.postgresql_v15.password
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.postgresql_v15.fqdn
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = var.postgresql_flexible_server_port
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = var.database_name
  key_vault_id = module.key-vault.key_vault_id
}
