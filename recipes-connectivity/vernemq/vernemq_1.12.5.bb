SUMMARY = "A distributed MQTT message broker based on Erlang/OTP. Built for high quality & Industrial use cases."

DESCRIPTION = "VerneMQ is a high-performance, distributed MQTT message broker. It scales horizontally \
and vertically on commodity hardware to support a high number of concurrent publishers and consumers \
while maintaining low latency and fault tolerance. VerneMQ is the reliable message hub for your IoT \
platform or smart products."

HOMEPAGE = "https://vernemq.com"

SECTION = "network"

DEPENDS = "erlang-native rebar3-native curl-native \
           erlang openssl snappy \
          "

LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=11fe9db289207b0e40d83d07f5e0727e"

SRC_URI = "git://github.com/vernemq/vernemq;protocol=https;branch=master \
           file://vars.config \
           file://vm.args \
           file://vernemq.conf \
           file://vernemq-volatiles.conf \
           file://vernemq.service \
           file://vernemq.init \
           file://volatiles.99_vernemq \
           file://rebar.config.eleveldb \
           file://build_deps.sh.eleveldb \
           file://Makefile.c_src.eleveldb \
           file://Makefile.c_src.bcrypt \
           file://Makefile.c_src.vmq_passwd \
          "

SRCREV = "50adf26e093a4d5a073964be2b322494e411a126"
PR = "r0"

S = "${WORKDIR}/git"

inherit rebar3-brokensep useradd update-rc.d systemd

REBAR3_RELEASE_NAME = "vernemq"
REBAR3_PROFILE = "default"

do_compile:prepend() {
    # fix dependency: eleveldb
    install -D ${WORKDIR}/build_deps.sh.eleveldb ${REBAR_BASE_DIR}/default/lib/eleveldb/c_src/build_deps.sh
    install -D ${WORKDIR}/Makefile.c_src.eleveldb ${REBAR_BASE_DIR}/default/lib/eleveldb/c_src/Makefile
    install -D ${WORKDIR}/rebar.config.eleveldb ${REBAR_BASE_DIR}/default/lib/eleveldb/rebar.config

    # fix dependency: bcrypt
    install -D ${WORKDIR}/Makefile.c_src.bcrypt ${REBAR_BASE_DIR}/default/lib/bcrypt/c_src/Makefile

    # fix dependency: vmq_passwd
    install -D ${WORKDIR}/Makefile.c_src.vmq_passwd ${S}/apps/vmq_passwd/c_src/Makefile
}

do_install:prepend() {
    cd ${S}
    cat ${WORKDIR}/vars.config > vars.generated
    echo "{app_version, \"${PV}\"}." >> vars.generated
}

do_install:append() {
    install -d ${D}${sysconfdir}/vernemq
    install -m 0644 ${WORKDIR}/vm.args ${D}${sysconfdir}/vernemq/vm.args
    install -m 0644 ${WORKDIR}/vernemq.conf ${D}${sysconfdir}/vernemq/vernemq.conf
    chown -R vernemq.vernemq ${D}${sysconfdir}/vernemq

    if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
        install -d ${D}${systemd_unitdir}/system
        install -m 0644 ${WORKDIR}/vernemq.service ${D}${systemd_unitdir}/system
        install -d ${D}${sysconfdir}/tmpfiles.d/
        install -m 0644 ${WORKDIR}/vernemq-volatiles.conf ${D}${sysconfdir}/tmpfiles.d/vernemq.conf
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES','sysvinit','true','false',d)}; then
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/vernemq.init ${D}${sysconfdir}/init.d/vernemq
        install -d ${D}${sysconfdir}/default/volatiles
        install -m 0644 ${WORKDIR}/volatiles.99_vernemq ${D}${sysconfdir}/default/volatiles/99_vernemq
    fi
}

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "--system --create-home --home-dir ${localstatedir}/lib/vernemq \
    --shell /bin/false --user-group vernemq"

SYSTEMD_SERVICE:${PN} = "vernemq.service"

INITSCRIPT_NAME = "vernemq"
INITSCRIPT_PARAMS = "defaults"
