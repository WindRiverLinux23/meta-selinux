SUMMARY = "User space tools for kernel auditing"
DESCRIPTION = "The audit package contains the user space utilities for \
storing and searching the audit records generated by the audit subsystem \
in the Linux kernel."
HOMEPAGE = "http://people.redhat.com/sgrubb/audit/"
SECTION = "base"
PR = "r8"
LICENSE = "GPLv2+ & LGPLv2+"
LIC_FILES_CHKSUM = "file://COPYING;md5=94d55d512a9ba36caa9b7df079bae19f"

SRC_URI = "http://people.redhat.com/sgrubb/audit/audit-${PV}.tar.gz \
           file://audit-python-configure.patch \
           file://audit-python.patch \
           file://fix-swig-host-contamination.patch \
           file://auditd \
           file://auditd.service \
           file://audit-volatile.conf \
"
SRC_URI[md5sum] = "544d863af2016b76afd8d1691b251164"
SRC_URI[sha256sum] = "9c914704fecc602e143e37152f3efbab2469692684c1a8cc1b801c1b49c7abc6"

SRC_URI_append_arm = "file://add-system-call-table-for-ARM.patch"

inherit autotools pythonnative update-rc.d systemd

UPDATERCPN = "auditd"
INITSCRIPT_NAME = "auditd"
INITSCRIPT_PARAMS = "defaults"

SYSTEMD_SERVICE_${PN} = "auditd.service"

DEPENDS += "python tcp-wrappers libcap-ng linux-libc-headers (>= 2.6.30)"

EXTRA_OECONF += "--without-prelude \
	--with-libwrap \
	--enable-gssapi-krb5=no \
	--with-libcap-ng=yes \
	--with-python=yes \
	--libdir=${base_libdir} \
	--sbindir=${base_sbindir} \
        --without-python3 \
        --disable-zos-remote \
	"
EXTRA_OECONF_append_arm = " --with-arm=yes"

EXTRA_OEMAKE += "PYLIBVER='python${PYTHON_BASEVERSION}' \
	PYINC='${STAGING_INCDIR}/$(PYLIBVER)' \
	pyexecdir=${libdir}/python${PYTHON_BASEVERSION}/site-packages \
	STDINC='${STAGING_INCDIR}' \
	"

SUMMARY_audispd-plugins = "Plugins for the audit event dispatcher"
DESCRIPTION_audispd-plugins = "The audispd-plugins package provides plugins for the real-time \
interface to the audit system, audispd. These plugins can do things \
like relay events to remote machines or analyze events for suspicious \
behavior."

PACKAGES =+ "audispd-plugins"
PACKAGES += "auditd ${PN}-python"

FILES_${PN} = "${sysconfdir}/libaudit.conf ${base_libdir}/libaudit.so.1* ${base_libdir}/libauparse.so.*"
FILES_auditd += "${bindir}/* ${base_sbindir}/* ${sysconfdir}/*"
FILES_audispd-plugins += "${sysconfdir}/audisp/audisp-remote.conf \
	${sysconfdir}/audisp/plugins.d/au-remote.conf \
	${sbindir}/audisp-remote ${localstatedir}/spool/audit \
	"
FILES_${PN}-dbg += "${libdir}/python${PYTHON_BASEVERSION}/*/.debug"
FILES_${PN}-python = "${libdir}/python${PYTHON_BASEVERSION}"
FILES_${PN}-dev += "${base_libdir}/*.so ${base_libdir}/*.la ${base_libdir}/pkgconfig/*"

CONFFILES_auditd += "${sysconfdir}/audit/audit.rules"
RDEPENDS_auditd += "bash"

do_install_append() {
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.a
	rm -f ${D}/${libdir}/python${PYTHON_BASEVERSION}/site-packages/*.la

	# reuse auditd config
	[ ! -e ${D}/etc/default ] && mkdir ${D}/etc/default
	mv ${D}/etc/sysconfig/auditd ${D}/etc/default
	rmdir ${D}/etc/sysconfig/

	# replace init.d
	install -D -m 0755 ${S}/../auditd ${D}/etc/init.d/auditd
	rm -rf ${D}/etc/rc.d

	if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
		install -d ${D}${sysconfdir}/tmpfiles.d/
		install -m 0644 ${WORKDIR}/audit-volatile.conf ${D}${sysconfdir}/tmpfiles.d/
	fi
	
	# install systemd unit files
	install -d ${D}${systemd_unitdir}/system
	install -m 0644 ${WORKDIR}/auditd.service ${D}${systemd_unitdir}/system

	chmod 750 ${D}/etc/audit ${D}/etc/audit/rules.d
	chmod 640 ${D}/etc/audit/auditd.conf ${D}/etc/audit/rules.d/audit.rules

	# Based on the audit.spec "Copy default rules into place on new installation"
	cp ${D}/etc/audit/rules.d/audit.rules ${D}/etc/audit/audit.rules
}
