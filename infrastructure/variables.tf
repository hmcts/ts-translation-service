// Infrastructural variables
variable "product" {
}

variable "component" {
}

variable "env" {
}

variable "location" {
  default = "UK South"
}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "tenant_id" {
  description = "(Required) The Azure Active Directory tenant ID that should be used for authenticating requests to the key vault. This is usually sourced from environment variables and not normally required to be specified."
}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}


////////////////////////////////
// Database
////////////////////////////////

// Define the default values for optional parameters
variable "sku_name" {
  default = "GP_Gen5_2"
}

variable "sku_tier" {
  default = "GeneralPurpose"
}

variable "sku_capacity" {
  default = "2"
}

variable "storage_mb" {
  default = "51200"
}

variable "ssl_enforcement" {
  default = "Enabled"
}

variable "backup_retention_days" {
  default = "35"
}

variable "georedundant_backup" {
  default = "Enabled"
}

// Define the values for mandatory/required parameters

variable "postgresql_user" {
  default = "ts"
}

variable "database_name" {
  default = "ts_translation_service"
}

variable "postgresql_version" {
  default = "11"
}

variable "pgsql_sku" {
  description = "The PGSql flexible server instance sku"
  default     = "GP_Standard_D2s_v3"
}

variable "aks_subscription_id" {}

variable "pgsql_storage_mb" {
  description = "Max storage allowed for the PGSql Flexibile instance"
  type        = number
  default     = 65536
}
variable "postgresql_flexible_server_port" {
  default = "5432"
}

variable "enable_schema_ownership" {
  type        = bool
  default     = false
  description = "Enables the schema ownership script. Change this to true if you want to use the script. Defaults to false"
}

variable "force_schema_ownership_trigger" {
  default     = ""
  type        = string
  description = "Update this to a new value to force the schema ownership script to run again."
}

variable "kv_subscription" {
  default     = "DCD-CNP-DEV"
  type        = string
  description = "Update this with the name of the subscription where the single server key vault is. Defaults to DCD-CNP-DEV."
}