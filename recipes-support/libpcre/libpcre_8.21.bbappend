PRINC = "1"

do_install_append () {
	if [ ! ${D}${libdir} -ef ${D}${base_libdir} ]; then
		mkdir -p ${D}/${base_libdir}/
		mv -f ${D}${libdir}/libpcre.so.* ${D}${base_libdir}/
		relpath=${@os.path.relpath("${base_libdir}", "${libdir}")}
		ln -sf ${relpath}/libpcre.so.0.0.1 ${D}${libdir}/libpcre.so
	fi
}

FILES_${PN} += "${base_libdir}/libpcre.so.*"
