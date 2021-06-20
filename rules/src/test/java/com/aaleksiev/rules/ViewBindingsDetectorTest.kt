package com.aaleksiev.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.aaleksiev.rules.stubs.fragment
import com.aaleksiev.rules.stubs.fragmentBlankBinding
import com.aaleksiev.rules.stubs.fragmentLayout
import com.aaleksiev.rules.stubs.viewBinding

@Suppress("UnstableApiUsage")
@RunWith(JUnit4::class)
class ViewBindingsDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = ViewBindingsDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(ViewBindingsDetector.ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, fragment, viewBinding, fragmentLayout, fragmentBlankBinding)
            .run()
    }

    @Test
    fun `lint check will pass when binding is not accessed`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onSaveInstanceState(outState: Bundle) {
                        super.onSaveInstanceState(outState)
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expectClean()
    }

    @Test
    fun `lint check will pass when binding is accessed in onViewCreated`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                        binding.imageView.alpha = .5f
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expectClean()
    }

    @Test
    fun `lint check will fail when binding is accessed in onSaveInstanceState`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onSaveInstanceState(outState: Bundle) {
                        super.onSaveInstanceState(outState)
                        binding.imageView.alpha = .5f
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expect(
            """
                src/com/test/BlankFragment.kt:12: Error: [onSaveInstanceState] Should not attempt to access view bindings after Fragment view was destroyed. [AccessDestroyedView]
                        binding.imageView.alpha = .5f
                        ~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
        )
    }

    @Test
    fun `lint check will fail when binding is accessed in onCreate`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        binding.imageView.alpha = .5f
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expect(
            """
                src/com/test/BlankFragment.kt:12: Error: [onCreate] Should not attempt to access view bindings after Fragment view was destroyed. [AccessDestroyedView]
                        binding.imageView.alpha = .5f
                        ~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
        )
    }

    @Test
    fun `lint check will fail when binding is accessed in onCreateView`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onCreateView(
                        inflater: LayoutInflater,
                        container: ViewGroup?,
                        savedInstanceState: Bundle?
                    ): View? {
                        binding.imageView.alpha = .5f
                        return super.onCreateView(inflater, container, savedInstanceState)
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expect(
            """
                src/com/test/BlankFragment.kt:15: Error: [onCreateView] Should not attempt to access view bindings after Fragment view was destroyed. [AccessDestroyedView]
                        binding.imageView.alpha = .5f
                        ~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
        )
    }

    @Test
    fun `lint check will fail when binding is accessed in onDestroy`() {
        check(
            kotlin(
                """
                package com.test

                import android.os.Bundle
                import androidx.fragment.app.Fragment
                
                class BlankFragment : Fragment() {
                
                    val binding by lazy { FragmentBlankBinding.bind(requireView()) }
                
                    override fun onDestroy() {
                        super.onDestroy()
                        binding.imageView.alpha = .5f
                    }
                
                    companion object {
                        fun newInstance() = BlankFragment()
                    }
                }
                """.trimIndent()
            ).indented()
        ).expect(
            """
                src/com/test/BlankFragment.kt:12: Error: [onDestroy] Should not attempt to access view bindings after Fragment view was destroyed. [AccessDestroyedView]
                        binding.imageView.alpha = .5f
                        ~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
        )
    }
}