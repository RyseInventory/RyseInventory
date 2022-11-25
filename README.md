# RyseInventory 1.8-1.19 ![](https://i.imgur.com/BS3gwxL.png)

Inventory System inspired by [SmartInventory](https://github.com/MinusKube/SmartInvs)!

This API is used to create and manage inventories in Minecraft more easily. Across many methods, the inventory can be
managed easily. Besides, you don't have to worry about the pages, because they are managed automatically. Moreover, many
things can be done with the inventory. More about this under the
point [Features](https://github.com/Rysefoxx/RyseInventory#features)

# Features

- Update 1.1.8 now has an integrated animation system.
- You can change the inventory title during operation.
- You can select 9 different inventory types.
- Fixed page size independent of items.
- Possibility to edit individual pages as desired.
- You can prevent the inventory from being closed.
- Own events based on the inventory.
- You can adjust the delay of the scheduler.
- You can adjust the period of the scheduler.
- You can set after how many seconds the inventory should be closed automatically.
- You can set after how many seconds the inventory should be opened.
- You can set that cached data is transferred to the next page.
- You can set whether the inventory should be split or not.
- You can set options. e.g. No damage when the player has the inventory open. Or you can e.g. not remove the block under
  the player when the player has the inventory open (...)
- You can directly specify on the Intelligent Item whether the player can interact with the item and look at
  it. [What do you mean?](https://github.com/Rysefoxx/RyseInventory/wiki/IntelligentItem)
- You can set some items to be visible only on certain pages.
- Page system
- ...Much more :)

# Install

- Register the `InventoryManager` in the onEnable. After that execute `#invoke()`.

# Template

- You may use [this Paper Plugin Template](https://github.com/eazypaulCode/plugin-template) to create a Paper Userdev-plugin which has RyseInventory preinstalled.

# Dependency

_(Make sure you shade the API.)_
_In the newer versions you can specify the API in your plugin.yml under Libraries. As a result, you no longer need to shade the API._

### Gradle (Groovy)

```
repositories {
    mavenCentral()
    maven { url "https://s01.oss.sonatype.org/content/groups/public/" }
}
dependencies {
    implementation 'io.github.rysefoxx.inventory:RyseInventory-Plugin:1.5.4'
}
```

### Gradle (Kotlin)

```
repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/groups/public/") }
}
dependencies {
    implementation("io.github.rysefoxx.inventory:RyseInventory-Plugin:1.5.4")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>sonatype</id>
        <url>"https://s01.oss.sonatype.org/content/groups/public/"</url>
    </repository>
</repositories>

<dependency>
  <groupId>io.github.rysefoxx.inventory</groupId>
  <artifactId>RyseInventory-Plugin</artifactId>
  <version>1.5.4</version>
</dependency>
```

# Found an issue?

- Create a new issue with a detailed description. [Click here](https://github.com/Rysefoxx/RyseInventory/blob/master/.github/ISSUE_TEMPLATE/bug_report.md) for an example. Alternatively, find it in the source code at .github/ISSUE_TEMPLATE.
- You have an idea for a feature? Use label **enhancement**

# You need help?

* Everything important is described in the wiki. [Click here](https://github.com/Rysefoxx/RyseInventory/wiki)

# Examples

* [Click here](https://github.com/Rysefoxx/RyseInventory/tree/master/examples)

# ToDo

- Create a Gitbook Wiki

# License

This API is licensed under the MIT License.
See [**LICENSE**](https://github.com/Rysefoxx/RyseInventory/blob/master/LICENSE)

Copyright (c) 2022 Rysefoxx
