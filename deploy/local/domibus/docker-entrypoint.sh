#!/bin/sh
#
# Wire the multitenancy logback includes before starting Tomcat.
#
# Domibus discovers its domains from the directories under conf/domibus/domains, but its logging is
# not domain aware: logback.xml ships with a pair of marker comments and the operator is expected to
# add one <include> per domain by hand. Since our domains arrive as bind mounts, the list is only
# known at container start, so we derive it here instead of baking it into the image.
#
# Logback cannot do this itself - it has no wildcard include (verified against the logback 1.5.32
# shipped in the Domibus 5.2.1 WAR: a globbed <include> silently matches nothing).
#
# Idempotent: the whole marker block is rewritten on every start, so restarts and re-runs converge.

set -eu

CONF_DIR="${DOMIBUS_CONFIG_LOCATION}"
LOGBACK_FILE="${CONF_DIR}/logback.xml"
TEMPLATE_FILE="${CONF_DIR}/logback-domain-template.xml"
GENERATED_DIR="${CONF_DIR}/logback-domains"

START_MARKER="multitenancy: start include domains config files here"
END_MARKER="multitenancy: end include domains config files here"

configure_logback() {
    if [ ! -f "${LOGBACK_FILE}" ]; then
        echo "domibus-entrypoint: ${LOGBACK_FILE} not found, skipping logback configuration" >&2
        return 0
    fi

    if ! grep -q "${START_MARKER}" "${LOGBACK_FILE}" || ! grep -q "${END_MARKER}" "${LOGBACK_FILE}"; then
        echo "domibus-entrypoint: multitenancy markers not found in ${LOGBACK_FILE}, refusing to rewrite it" >&2
        return 1
    fi

    rm -rf "${GENERATED_DIR}"
    mkdir -p "${GENERATED_DIR}"

    includes_file="${GENERATED_DIR}/.includes"
    : > "${includes_file}"

    for domain_dir in "${CONF_DIR}"/domains/*/; do
        [ -d "${domain_dir}" ] || continue
        domain=$(basename "${domain_dir}")

        # A domain may ship its own fragment, e.g. to give one domain a different log level. Such a
        # fragment is bind mounted and therefore authoritative; only fall back to the template.
        fragment="${domain_dir}${domain}-logback.xml"
        if [ ! -f "${fragment}" ]; then
            if [ ! -f "${TEMPLATE_FILE}" ]; then
                echo "domibus-entrypoint: no fragment for domain [${domain}] and no template at ${TEMPLATE_FILE}" >&2
                return 1
            fi
            fragment="${GENERATED_DIR}/${domain}-logback.xml"
            sed "s/domain_name/${domain}/g" "${TEMPLATE_FILE}" > "${fragment}"
            echo "domibus-entrypoint: generated logback configuration for domain [${domain}]"
        else
            echo "domibus-entrypoint: using provided logback configuration for domain [${domain}]"
        fi

        printf '    <include optional="true" file="%s"/>\n' "${fragment}" >> "${includes_file}"
    done

    awk -v includes_file="${includes_file}" \
        -v start_marker="${START_MARKER}" \
        -v end_marker="${END_MARKER}" '
        index($0, start_marker) { print; while ((getline line < includes_file) > 0) print line; inside = 1; next }
        index($0, end_marker)   { inside = 0 }
        !inside                 { print }
    ' "${LOGBACK_FILE}" > "${LOGBACK_FILE}.tmp"

    mv "${LOGBACK_FILE}.tmp" "${LOGBACK_FILE}"
    rm -f "${includes_file}"
}

configure_logback

exec "$@"
