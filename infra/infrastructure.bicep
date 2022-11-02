@description('Desired Azure region for the resources to be deployed into.')
param region string = resourceGroup().location

@description('Name of the container image on the registry.')
param image_name string = 'crunchtimeapp/crunchtime'

@description('Name of the container image tag on the registry.')
param image_tag string = '1.0.1'

@description('Login username for the database')
@secure()
param dbLoginUserName string

@description('Login password for the database')
@secure()
param dbLoginPassword string

var base_name = 'gauchocourses'

resource app 'Microsoft.Web/sites@2022-03-01' = {
  location: region
  name: '${base_name}-app'
  properties: {
    serverFarmId: appServicePlan.id
    siteConfig: {
      linuxFxVersion: 'DOCKER|${image_name}:${image_tag}'
      appSettings: [
        { name: 'DOCKER_REGISTRY_SERVER_URL', value: 'https://index.docker.io' }
        { name: 'DOCKER_REGISTRY_SERVER_USERNAME', value: '' }
        { name: 'DOCKER_REGISTRY_SERVER_PASSWORD', value: null }
        { name: 'WEBSITES_ENABLE_APP_SERVICE_STORAGE', value: 'false' }
        { name: 'REDIS_ACCESS_KEY', value: cache.listKeys().primaryKey }
        { name: 'POSTGRES_USERNAME', value: dbLoginUserName }
        { name: 'POSTGRES_PASSWORD', value: dbLoginPassword }
      ]
    }
  }
}

resource appServicePlan 'Microsoft.Web/serverfarms@2022-03-01' = {
  location: region
  name: '${base_name}-appServicePlan'
  sku: {
    name: 'B1'
  }
  properties: {
    reserved: true
  }
}

resource cache 'Microsoft.Cache/redis@2022-06-01' = {
  location: region
  name: '${base_name}-cache'
  properties: {
    sku: {
      name: 'Basic'
      capacity: 1
      family: 'C'
    }
  }
}

resource databaseServer 'Microsoft.DBforPostgreSQL/flexibleServers@2021-06-01' = {
  location: 'eastus'
  name: '${base_name}-db-server2'
  properties: {
    administratorLogin: dbLoginUserName
    administratorLoginPassword: dbLoginPassword
    version: '12'
    createMode: 'Default'
    storage: {
      storageSizeGB: 32
    }
    highAvailability: {
      mode: 'Disabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
  }
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
}

resource dbFirewallRules 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2021-06-01' = {
  name: '${base_name}-db-firewall'
  parent: databaseServer
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2021-06-01' = {
  parent: databaseServer
  name: '${base_name}-db'
  properties: {
    charset: 'utf8'
    collation: 'en_US.utf8'
  }
}
