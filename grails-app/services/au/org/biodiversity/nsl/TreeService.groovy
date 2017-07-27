package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional

/**
 * The 2.0 Tree service. This service is the central location for all interaction with the tree.
 */
@Transactional
class TreeService implements ValidationUtils {

    /**
     * get the named tree. This is case insensitive
     * @param name
     * @return tree or null if not found
     */
    Tree getTree(String name) {
        mustHave('Tree name': name)
        Tree.findByNameIlike(name)
    }

    /**
     * get the current TreeElement for a name on the given tree
     * @param name
     * @param tree
     * @return treeElement or null if not on the tree
     */
    TreeElement findCurrentElementForName(Name name, Tree tree) {
        if (name && tree) {
            return findElementForName(name, tree.currentTreeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for a name in the given version of a tree
     * @param name
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    TreeElement findElementForName(Name name, TreeVersion treeVersion) {
        if (name && treeVersion) {
            return TreeElement.findByNameIdAndTreeVersion(name.id, treeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the current version of a tree
     * @param instance
     * @param tree
     * @return treeElement or null if not on the tree
     */
    TreeElement findCurrentElementForInstance(Instance instance, Tree tree) {
        if (instance && tree) {
            return TreeElement.findByInstanceIdAndTreeVersion(instance.id, tree.currentTreeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the given version of a tree
     * @param instance
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    TreeElement findElementForInstance(Instance instance, TreeVersion treeVersion) {
        if (instance && treeVersion) {
            return TreeElement.findByInstanceIdAndTreeVersion(instance.id, treeVersion)
        }
        return null
    }

    /**
     * get the tree path as a list of TreeElements
     * @param treeElement
     * @return List of TreeElements
     */
    List<TreeElement> getElementPath(TreeElement treeElement) {
        mustHave(treeElement: treeElement)
        treeElement.treePath.split('/').collect { String stringElementId ->
            TreeElement key = new TreeElement(treeVersion: treeElement.treeVersion, treeElementId: stringElementId.toBigInteger())
            TreeElement.get(key)
        }
    }

}