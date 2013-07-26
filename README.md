# OpenStreetMap Importer

![contributions](https://raw.github.com/insideout10/open-street-map-importer/master/banner-open-street-map-importer.png)

## Introduction

The OpenStreetMap Importer is tool written in Scala that:

1. loads an XML feed of POIs,
2. stores them in a local database,
3. validates the POIs against OpenStreetMap,
4. creates, updates, or deletes the corresponding nodes in OpenStreetMap (if validation succeeds).

## POIs feed format

The POIs feed format is inspired from the [OpenStreetMap XML](https://wiki.openstreetmap.org/wiki/OSM_XML). It's a set of data containing a list of nodes (POIs) - *note that the ID in the node element does not have any reference whatsoever with the ID in OpenStreetMap or any of the tags.*

```xml
<set name="charging-stations">
	<tags>
		<tag k="key_1" v="value_1" />
		<tag k="key_2" v="value_2" />
		<tag k="key_…" v="value_…" />
	</tags>
	<nodes>
		<node id="ENEL_00000026" lat="43.720898" lon="10.3892355">
			<tag k="key_1" v="value_1" />
			<tag k="key_2" v="value_2" />
			<tag k="key_…" v="value_…" />
			</node>
	</nodes>
</set>
```

### Set

The set element defines the data to be attached to an OpenStreetMap changeset. All the tags provided in the child *tags* element will be considered as tags of the changeset.

### Node

The node element is a POI. The attribute *id* defines the unique ID of the POI in the importer local database for operations on the same node. The attributes *lat* and *lon* define the coordinates of POI. All the tags provided in the children *tag* elements will be considered as tags of the node.

## Configuration

The configuration is stored in an `application.conf` file in HOCON format ( “Human-Optimized Config Object Notation”).

The location of configuration file is defined using the following command line parameter:
 `-Dconfig.file=/path/to/the/file/application.conf`
 
 The configuration file contains the following settings:
 
 ```
 app {
    # the user-agent used when making HTTP requests to OpenStreetMap
    user-agent          = "open-street-map-importer"  

	# the address/port the OpenStreetMap Importer binds to
    server {
        interface       = "localhost"
        port            = 8080
    }

	# the local database settings (tables will be created automatically the first time)
    database {
        url             = "jdbc:mysql://localhost:3306/database?user=username&password=password"
    }

	# the OpenStreetMap API settings
    open-street-map.api {
        url             = "http://api06.dev.openstreetmap.org/api/0.6"
        username        = "username"
        password        = "password"
        
        # max number of concurrent calls to OpenStreetMap
        max-threads     = 2
    }

	# the Overpass API settings
    overpass {
        url             = "http://overpass-api.de/api/interpreter"
        parameters      = "data=%5Btimeout%3A86400%5D%3B%20node%5Bamenity%3D%22charging_station%22%5D%28around%3A{{radius}}%2C{{latitude}}%2C{{longitude}}%29-%3E.b%3B%20.b%20out%20meta%3B"
        radius          = 100
    }
}
```

## Workflow

### New POIs

The importer looks for an existing POI in the local database by the POI id. If the POI is not found than a new record is created.

### Existing POIs

If a POI is found in the local database, then its tags are compared. If there's no different the POI will not be pushed to OpenStreetMap, *avoiding unnecessary operations and updates*.

### Validation

Before *making any operation* on OpenStreetMap, the POI is validated:

1. if it is an existing POI (i.e. it has an OpenStreetMap ID and version), then the local version is compared with the OpenStreetMap version. If they don't match, meaning that the node has been updated on OpenStreetMap by another mapper, or if the node is gone, meaning it has been deleted by another mapper, validation fails.
2. if it is a new POI, a check is made whether a node of the same kind (in our specific scenario *amenity=charging_station*) exists in a 100m. radius. If an existing node exists, validation fails.

When validation fails, an error is written in the log and the Importer **does not perform any operation on OpenStreetMap**.

### Result

When validation is successful, the Importer performs the operations on OpenStreetMap and stores in the local database the resulting OpenStreetMap ID and version.

## License

This software is released under the GNU General Public License, version 3.0 (GPLv3)
