package com.dotmarketing.factories;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.google.common.collect.Table;

import java.util.List;
import java.util.Set;

/**
 * API for {@link com.dotmarketing.beans.MultiTree}
 */
public interface MultiTreeAPI {

    /**
     * Saves MultiTrees for a given page
     * 
     * @param mTrees
     * @throws DotDataException
     */
    void saveMultiTrees(String pageId, List<MultiTree> mTrees) throws DotDataException;

    /**
     * Saves a specific MultiTree
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void saveMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Deletes a specific MultiTree
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void deleteMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Deletes a MultiTrees related to the identifier
     *
     * @param identifier {@link Identifier}
     * @throws DotDataException
     */
    void deleteMultiTreeByIdentifier(Identifier identifier) throws DotDataException;

    /**
     * Removes any mutlitree that has the identifiers as either a parent or as a child
     * 
     * @param inodes
     * @throws DotDataException
     */
    void deleteMultiTreesForIdentifiers(List<String> identifiers) throws DotDataException;


    /**
     * deletes all the multi tress related to the identifier, including parents and child in addition
     * for the pages related refresh the cache and publish relationships
     * 
     * @param identifier String
     * @throws DotDataException
     */
    void deleteMultiTreesRelatedToIdentifier(final String identifier) throws DotDataException;

    /**
     * This method returns ALL MultiTree entries (in all languages) for a given page. It is up to what
     * ever page renderer to properly choose which MultiTree children to show for example, show an
     * english content on a spanish page when language fallback=true or specific content for a given
     * persona.
     * 
     * @param page
     * @param liveMode
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Table<String, String, Set<String>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException;


    /**
     * Saves a list of MultiTrees
     * 
     * @param mTrees
     * @throws DotDataException
     */
    void saveMultiTrees(List<MultiTree> mTrees) throws DotDataException;

    /**
     * Deletes a list of MultiTrees
     * 
     * @param mTree
     * @throws DotDataException
     */
    void deleteMultiTree(List<MultiTree> mTree) throws DotDataException;

    /**
     * Deletes all MultiTrees that have either this page id or container id as a parent
     * 
     * @param pageOrContainer
     * @throws DotDataException
     */
    void deleteMultiTreeByParent(String pageOrContainer) throws DotDataException;

    /**
     * Deletes all MultiTrees that have contentlet id as a child
     * 
     * @param pageOrContainer
     * @throws DotDataException
     */
    void deleteMultiTreeByChild(String contentIdentifier) throws DotDataException;

    /**
     * Gets them all
     * 
     * @return
     */
    List<MultiTree> getAllMultiTrees();

    /**
     * Gets a list of all MultiTrees on a page (even if they are bad)
     * 
     * @param parentInode
     * @return
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByPage(String parentInode) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container+container instance
     * 
     * @param htmlPage
     * @param container
     * @param containerInstance
     * @return
     */
    List<MultiTree> getMultiTrees(String htmlPage, String container, String containerInstance);

    /**
     * Gets a list of MultiTrees that belong to the page+container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(String htmlPage, String container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container+container instance
     * 
     * @param htmlPage
     * @param container
     * @param containerInstance
     * @return
     */
    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container, String containerInstance);

    /**
     * Gets a list of MultiTrees that belong to the container on any page
     * 
     * @param container
     * @return
     */
    List<MultiTree> getContainerMultiTrees(String containerIdentifier) throws DotDataException;

    /**
     * gets all MultiTrees that have contentlet id as a child
     * 
     * @param pageOrContainer
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByChild(String contentIdentifier) throws DotDataException;

    /**
     * gets all MultiTrees that in a container of a particular content type
     * 
     * @param containerIdentifier
     * @param structureInode
     * @return
     */
    List<MultiTree> getContainerStructureMultiTree(String containerIdentifier, String structureInode);

    /**
     * gets the containers with MultiTree entries on a page (The containers may or may not exist)
     * 
     * @param pageId
     * @return
     * @throws DotDataException
     */
    List<String> getContainersId(String pageId) throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent, String containerInstance)
            throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent, String containerInstance) throws DotDataException;

    /**
     * Gets a specific MultiTree entry regardless of containerInstance
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent) throws DotDataException;

    /**
     * Gets a list of MultiTrees that have the Identifier as a Parent
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(Identifier parent) throws DotDataException;

    /**
     * Gets a list of MultiTrees that have the Identifiers as a Parent Page+Container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(Identifier htmlPage, Identifier container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that has the parentId as a parent
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(String parentId) throws DotDataException;

}
