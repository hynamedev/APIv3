/**
 * Uses maven-shade-plugin to override classes in fr.javatic.mongo:mongo-jackson-codec
 *
 * Similar to what's done with org.apache.commons:commons-pool2 in qLib, we modify and create
 * classes in this codebase in order to override fr.javatic.mongo.jacksonCodec classes in the final jar.
 *
 * These changes add an @Entity annotation and fix serializers in the @Id annotation.
 * Without these changes the mongodb driver fails to deserialize documents. Additionally, an @Entity
 * annotation allows us to selectively include types, instead of mapping ALL types.
 *
 * The files present here were copied from:
 * https://github.com/FrozenOrb/mongo-jackson-codec/commit/b3a168e5f5f7464c3721fd9ebc81a4cca7d4db2f (entire common)
 * https://github.com/FrozenOrb/mongo-jackson-codec/commit/20c161337900bb7daabd3d9d99ead1be35c0c41b (non-build files only)
 */
package fr.javatic.mongo.jacksonCodec;