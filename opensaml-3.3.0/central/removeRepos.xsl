<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:mvn="http://maven.apache.org/POM/4.0.0">
 <xsl:template match="mvn:repositories"/>
 <xsl:template match="mvn:pluginRepositories"/>

 <xsl:template match="*|@*|node()">
   <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
   </xsl:copy>
 </xsl:template>



</xsl:stylesheet>
