/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.api.stream;

import java.util.Map;
import java.util.Set;

import org.red5.server.api.statistics.IClientBroadcastStreamStatistics;

/**
 * A broadcast stream that comes from client.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IClientBroadcastStream extends IClientStream, IBroadcastStream {

    /**
     * Notify client that stream is ready for publishing.
     */
    void startPublishing();

    /**
     * Return statistics about the stream.
     * 
     * @return statistics
     */
    IClientBroadcastStreamStatistics getStatistics();

    /**
     * Sets streaming parameters as supplied by the publishing application.
     * 
     * @param params
     *            parameter map
     */
    void setParameters(Map<String, String> params);

    /**
     * Returns streaming parameters.
     * 
     * @return parameters
     */
    Map<String, String> getParameters();

    /**
     * Adds a stream name alias.
     * 
     * @param alias
     * @return true if added to the aliases, false otherwise
     */
    boolean addAlias(String alias);

    /**
     * Returns whether or not an alias for this stream exists.
     * 
     * @return true if an alias has been added and false otherwise
     */
    boolean hasAlias();

    /**
     * Returns an alias.
     * 
     * @return alias if at least one exists or null when there are none
     */
    String getAlias();

    /**
     * Returns whether or not a given alias exists.
     * 
     * @param alias
     * @return true if found and false otherwise
     */
    boolean containsAlias(String alias);

    /**
     * Returns all the aliases.
     * 
     * @return all aliases for this instance or an empty set
     */
    Set<String> getAliases();

}
