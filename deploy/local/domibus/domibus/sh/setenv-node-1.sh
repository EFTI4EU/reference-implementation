export CATALINA_HOME=/opt/domibus
export CATALINA_TMPDIR=/opt/domibus/temp
export JAVA_OPTS="$JAVA_OPTS -Xms128m -Xmx1024m"
export JAVA_OPTS="$JAVA_OPTS -Ddomibus.config.location=$CATALINA_HOME/conf/domibus"
export JAVA_OPTS="$JAVA_OPTS -Ddomibus.node.id=01"
