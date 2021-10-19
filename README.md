# XProc 3.0 paged media steps

This repository contains the XML Calabash 3.x implementation of the paged media steps,
`p:css-formatter` and `p:xsl-formatter`.

CSS rendering is supported with the [Antenna House](https://www.antennahouse.com/)
or [Prince](https://www.princexml.com/) formatters.

XSL rendering is supported with the  [Antenna House](https://www.antennahouse.com/),
[RenderX](http://www.renderx.com/tools/xep.html), or
[Apache FOP](https://xmlgraphics.apache.org/fop/) formatters.

Configure the renderer you wish to use in your `.xmlcalabash` configuration file:

* For AntennaHouse CSS:
```
<cc:css-processor>com.xmlcalabash.paged.processors.CssAH</cc:css-processor>
```
* For Prince CSS:
```
<cc:css-processor>com.xmlcalabash.paged.processors.CssPrince</cc:css-processor>
```

There are no open-source CSS formatters (that I’m aware of), so there
is no default CSS implementation.

* For AntennaHouse XSL:
```
<cc:fo-processor>com.xmlcalabash.paged.processors.FoAH</cc:fo-processor>
```
* For RenderX XSL:
```
<cc:fo-processor>com.xmlcalabash.paged.processors.FoXEP</cc:fo-processor>
```
* For Apache FOP XSL:
```
<cc:fo-processor>com.xmlcalabash.paged.processors.FoFOP</cc:fo-processor>
```

The defalut implementation is Apache FOP.

## Note

This project won’t compile if you don’t have the API jar files for the
various processors. It’s not clear to me that I’m allowed to
redistribute them, so I’m not exactly sure what to do.
