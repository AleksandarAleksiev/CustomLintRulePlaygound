@file:Suppress("UnstableApiUsage")

package com.aaleksiev.rules.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.*

val fragment: TestFile = java(
    """
        package androidx.fragment.app;
        
        public class Fragment {
            public Fragment() {
            }
        }
    """.trimIndent()
)

val fragmentLayout: TestFile = xml(
    "res/layout/fragment_blank.xml",
    """
        <?xml version="1.0" encoding="utf-8"?>
        <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:tools="http://schemas.android.com/tools"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:viewBindingIgnore="true">

            <ImageView
                android:id="@+id/image_view"
                android:layout_width="240dp"
                android:layout_height="240dp"
                android:src="@drawable/ic_launcher_background"/>

        </FrameLayout>
    """.trimIndent()
).indented()

val viewBinding: TestFile = java(
    """
        package androidx.viewbinding;

        import android.view.View;
        import androidx.annotation.NonNull;

        /** A type which binds the views in a layout XML to fields. */
        public interface ViewBinding {
            /**
             * Returns the outermost {@link View} in the associated layout file. If this binding is for a
             * {@code <merge>} layout, this will return the first view inside of the merge tag.
             */
            @NonNull
            View getRoot();
        }
    """.trimIndent()
)

val fragmentBlankBinding: TestFile = kotlin(
    """
        package com.test

        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import android.widget.FrameLayout
        import android.widget.ImageView
        import androidx.viewbinding.ViewBinding
        
        class FragmentBlankBinding private constructor(
            private val rootView: FrameLayout,
            val imageView: ImageView
        ) : ViewBinding {
            override fun getRoot(): FrameLayout {
                return rootView
            }
        
            companion object {
                @JvmOverloads
                fun inflate(
                    inflater: LayoutInflater,
                    parent: ViewGroup? = null, attachToParent: Boolean = false
                ): FragmentBlankBinding {
                    val root = inflater.inflate(R.layout.fragment_blank, parent, false)
                    if (attachToParent) {
                        parent!!.addView(root)
                    }
                    return bind(root)
                }
        
                fun bind(rootView: View): FragmentBlankBinding = FragmentBlankBinding(
                    rootView = rootView as FrameLayout,
                    imageView = requireNotNull(rootView.findViewById(R.id.image_view)) { "Missing R.id.image_view" }
                )
            }
        }
        """.trimIndent()
).indented()