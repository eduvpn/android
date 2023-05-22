package nl.eduvpn.app.utils

interface Listener {
    /**
     * @param   o     the object calling `update`
     * @param   arg   an optional argument
     * method.
     */
    fun update(o: Any, arg: Any?)
}
