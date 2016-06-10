package org.camunda.bpm.camel.common;

public class UriUtils {

    public static class ParsedUri {

        private final String remainingUri;

        private final UriType type;

        private final String[] components;

        /**
         * @param remainingUri
         *            the remaining part of the URI without the query parameters
         *            or component prefix
         */
        public ParsedUri(final String remainingUri) {

            this.remainingUri = remainingUri;

            components = parseUri(remainingUri);
            if ((components == null) || (components.length == 0)) {
                throw new RuntimeException("Cannot create a producer for URI '" + remainingUri + "'");
            }

            final String identifier = components[0];
            type = UriType.typeByIdentifier(identifier);

        }

        public String[] getComponents() {
            return components;
        }

        public UriType getType() {
            return type;
        }

        /**
         * @return the remaining part of the URI without the query parameters or
         *         component prefix
         */
        public String getRemainingUri() {
            return remainingUri;
        }

    }

    public static enum UriType {

        StartProcess("start"), SendSignal("signal"), SendMessage("message"), ExternalTask("externalTask");

        private String identifier;

        private UriType(final String identifier) {
            this.identifier = identifier;
        }

        public static UriType typeByIdentifier(final String identifier) {

            for (final UriType type : values()) {
                if (type.identifier.equals(identifier)) {
                    return type;
                }
            }

            throw new RuntimeException("Cannot create a producer for identifier '" + identifier + "'");

        }

    };

    public static String[] parseUri(String remainingUri) {

        return remainingUri.split("/");

    }

}
