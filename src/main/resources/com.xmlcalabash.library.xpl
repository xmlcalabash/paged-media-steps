<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">

<p:declare-step type="p:css-formatter">
     <p:input port="source" content-types="xml html"/>
     <p:input port="stylesheet" content-types="text" sequence="true">
          <p:empty/>
     </p:input>
     <p:output port="result" content-types="any"/>
     <p:option name="parameters" as="map(xs:QName,item()*)?"/>     
     <p:option name="content-type" as="xs:string?"/>               
</p:declare-step>

<p:declare-step type="p:xsl-formatter">
     <p:input port="source" content-types="xml"/>
     <p:output port="result" content-types="any"/>
     <p:option name="parameters" as="map(xs:QName,item()*)?"/>     
     <p:option name="content-type" as="xs:string?"/>               
</p:declare-step>

</p:library>
