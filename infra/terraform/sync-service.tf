resource "azurerm_servicebus_namespace" "main" {
  name                = "${local.resource_prefix}-sb-${local.suffix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Standard"
  tags                = local.tags
}

resource "azurerm_servicebus_queue" "sync_events" {
  name         = "sync-events"
  namespace_id = azurerm_servicebus_namespace.main.id
}

output "service_bus_namespace_name" {
  value = azurerm_servicebus_namespace.main.name
}

output "sync_service_application_insights_connection_string" {
  value     = azurerm_application_insights.main.connection_string
  sensitive = true
}
