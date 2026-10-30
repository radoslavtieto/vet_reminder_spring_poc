resource "azurerm_subnet" "mysql" {
  name                 = "mysql"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.20.2.0/24"]

  delegation {
    name = "mysql"

    service_delegation {
      name = "Microsoft.DBforMySQL/flexibleServers"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
      ]
    }
  }
}

resource "azurerm_private_dns_zone" "mysql" {
  name                = "private.mysql.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "mysql" {
  name                  = "${local.resource_prefix}-mysql-dns-link"
  private_dns_zone_name = azurerm_private_dns_zone.mysql.name
  virtual_network_id    = azurerm_virtual_network.main.id
  resource_group_name   = azurerm_resource_group.main.name
  registration_enabled  = false
  tags                  = local.tags
}

resource "azurerm_mysql_flexible_server" "main" {
  name                   = "${local.resource_prefix}-mysql-${local.suffix}"
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  version                = "8.0.21"
  delegated_subnet_id    = azurerm_subnet.mysql.id
  private_dns_zone_id    = azurerm_private_dns_zone.mysql.id
  administrator_login    = var.mysql_admin_username
  administrator_password = var.mysql_admin_password
  sku_name               = "B_Standard_B1ms"
  backup_retention_days  = 7

  storage {
    size_gb = 32
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.mysql]
  tags       = local.tags
}

resource "azurerm_mysql_flexible_database" "pms" {
  name                = "vetsync"
  resource_group_name = azurerm_resource_group.main.name
  server_name         = azurerm_mysql_flexible_server.main.name
  charset             = "utf8mb4"
  collation           = "utf8mb4_0900_ai_ci"
}

output "mysql_fqdn" {
  value = azurerm_mysql_flexible_server.main.fqdn
}

output "pms_api_application_insights_connection_string" {
  value     = azurerm_application_insights.main.connection_string
  sensitive = true
}

variable "mysql_admin_username" {
  description = "Administrator username for Azure Database for MySQL."
  type        = string
  default     = "vetsyncadmin"
}

variable "mysql_admin_password" {
  description = "Administrator password for Azure Database for MySQL. Supply via TF_VAR_mysql_admin_password or a local, untracked tfvars file."
  type        = string
  sensitive   = true
}
