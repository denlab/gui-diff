(ns gui-diff.core
  (:require 
            [clojure.java.shell :as sh]
            [clojure.test :as ct]
            [gui-diff.internal :as i])
  (:import java.io.File))


(defn gui-diff
  "Display a visual diff of two data structures, a and b. On Mac uses FileMerge.
   On Linux, first tries to use Meld, then falls back to diff."
  [a b]
  (let [file-1 (File/createTempFile "a_gui_diff" ".txt")
        file-2 (File/createTempFile "b_gui_diff" ".txt")]
    (spit file-1 (i/p-str a))
    (spit file-2 (i/p-str b))
    (i/diff-files file-1 file-2)))

(defn- ct-output->gui-diff-report [ct-report-str]
  (-> ct-report-str
      i/ct-report-str->failure-maps
      i/failure-maps->gui-diff-report-left-and-right-side))

(defn upgrade-to-gui-diff
  "Wrap any function that sends failure information to clojure.test's *test-out*
   and returns a new function that creates visual diff report of each failure
   that occurs when it is called."
  [test-fn]
  (fn [& args]
    (let [sw (java.io.StringWriter.)]
      (binding [ct/*test-out* sw]
        (apply test-fn args))

      (let [[expecteds actuals]  (ct-output->gui-diff-report (str sw))]
        (when-not (empty? expecteds)
          (let [file-1 (File/createTempFile "a_gui_diff_report" ".txt")
                file-2 (File/createTempFile "b_gui_diff_report" ".txt")]
            (spit file-1 expecteds)
            (spit file-2 actuals)
            (i/diff-files file-1 file-2))))
      
      (print (str sw)))))

(defmacro with-gui-diff
  "Wrap any code that sends failure information to clojure.test's *test-out*
   to create visual diff report of each failure."
  [& body]
  `((upgrade-to-gui-diff (fn [] ~@body))))

(def ^{:doc "Wraps clojure.test/run-tests to generate a gui-diff report"}
  run-tests++
  (upgrade-to-gui-diff ct/run-tests))

(def ^{:doc "Wraps clojure.test.run-all-tests to generate a gui-diff report"}
  run-all-tests++
  (upgrade-to-gui-diff ct/run-all-tests))