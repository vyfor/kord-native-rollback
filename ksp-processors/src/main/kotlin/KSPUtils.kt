package dev.kord.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

internal fun Resolver.getNewClasses() = getNewFiles().flatMap { it.declarations.filterIsInstance<KSClassDeclaration>() }

@OptIn(KspExperimental::class)
internal inline fun <reified A : Annotation> KSAnnotated.getAnnotationsByType() = getAnnotationsByType(A::class)
