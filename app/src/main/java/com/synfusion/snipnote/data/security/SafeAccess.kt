package com.synfusion.snipnote.data.security

/** Thrown when code attempts to touch Safe content while the workspace is locked. */
class SafeLockedException : IllegalStateException("The Safe workspace is locked")
