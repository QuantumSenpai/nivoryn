import importlib.util
import pathlib
import unittest
spec=importlib.util.spec_from_file_location('observer',pathlib.Path(__file__).with_name('observe-process.py'))
observer=importlib.util.module_from_spec(spec); spec.loader.exec_module(observer)
class ObserverTest(unittest.TestCase):
    def test_ordinary_snapshot_is_not_called_malware(self):
        self.assertEqual([],observer.classify({'temporaryExecutable':False,'writableExecutableMappings':0,'openSocketDescriptors':2}))
    def test_observations_have_explicit_benign_alternatives(self):
        findings=observer.classify({'temporaryExecutable':True,'writableExecutableMappings':1,'openSocketDescriptors':40})
        self.assertEqual(3,len(findings))
        self.assertFalse(any('confirmed' in finding for finding in findings))
if __name__=='__main__': unittest.main()
