# 🐳 Whale Protocol
Whale Protocol es una implementación de un protocolo que describe una comunicación cliente y servidor desarrollada en el lenguaje de programación Java, es el resultado del desarrollo del primer reto de Topicos Especiales de Telemática. Este sistema está diseñado específicamente para la transferencia de archivos donde se manejan ciertas características independientes de la aplicación y se realiza tanto un desarrollo para el cliente como para el servidor los cuales hacen parte del mismo aplicativo. Este sistema destaca por simular una red P2P del lado de los servidores, de tal modo que al buscar un archivo en la red este será buscado en cada uno de los servidores que esten encadenados o por ejemplo, en caso de subir un archivo la red buscará el servidor con la suficiente capacidad para guardarlo ya que a cada nodo o _Whale_ se le puede especificar un valor máximo.

### Comandos del Cliente

- `help` – Permite ver una lista con todos los comandos del cliente. Ejemplo de uso: _help_
- `upload` – Permite  subir un archivo a la red.
- `download` - Permite descargar un archivo de la red.
- `find` - Permite buscar un archivo en la red por su nombre o su HASH.

### Desarrolladores
- Abraham M. Lora (ToxicSSJ)