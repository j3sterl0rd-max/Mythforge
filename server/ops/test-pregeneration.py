"""Offline regression checks: no real RCON requests, world edits or ledger writes."""
from pathlib import Path
import importlib.util
import unittest
import tempfile
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location('pregen', Path(__file__).with_name('pregenerate-worlds.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SupervisorTests(unittest.TestCase):
    def tearDown(self):
        module.close_connection()

    def test_reuses_authenticated_connection(self):
        stream = Mock()
        with patch.object(module.socket, 'create_connection', return_value=stream) as connect, patch.object(module.rcon, 'request', return_value='ok') as request:
            self.assertEqual(module.console('list'), 'ok')
            self.assertEqual(module.console('tick query'), 'ok')
            self.assertEqual(connect.call_count, 1)
            self.assertEqual([call.args[2] for call in request.call_args_list], [3, 2, 2])
            self.assertEqual([call.args[1] for call in request.call_args_list], [1, 2, 3])

    def test_disconnect_closes_and_next_request_reauthenticates(self):
        first, second = Mock(), Mock()
        with patch.object(module.socket, 'create_connection', side_effect=[first, second]) as connect, patch.object(module.rcon, 'request', side_effect=['auth', EOFError(), 'auth', 'players']):
            with self.assertRaises(EOFError):
                module.console('list')
            first.close.assert_called_once()
            self.assertEqual(module.console('list'), 'players')
            self.assertEqual(connect.call_count, 2)

    def test_intentional_player_pause_is_not_a_stall(self):
        state = {'active': 'minecraft:overworld', 'completedTasks': [], 'paused': True, 'lastChunkCount': 123, 'lastProgressTime': 0}
        replies = ['There are 1 of a max of 30 players online: Fixture', 'Task running for minecraft:overworld. Processed: 123 chunks', 'paused']
        with patch.object(module, 'console', side_effect=replies) as console, patch.object(module, 'save'), patch.object(module.time, 'time', return_value=1000), patch.object(module.shutil, 'disk_usage', return_value=Mock(free=100*1024**3)), patch.object(Path, 'exists', return_value=False):
            self.assertTrue(module.step(state))
            self.assertEqual(state['status'], 'paused for players')
            self.assertEqual(console.call_args_list[-1].args[0], 'chunky pause')

    def test_real_generation_stall_still_stops(self):
        state = {'active': 'minecraft:overworld', 'completedTasks': [], 'paused': False, 'lastChunkCount': 123, 'lastProgressTime': 0}
        with patch.object(module, 'console', side_effect=['There are 0 of a max of 30 players online:', 'Task running for minecraft:overworld. Processed: 123 chunks', 'paused']), patch.object(module, 'save'), patch.object(module.time, 'time', return_value=1000):
            self.assertFalse(module.step(state))
            self.assertIn('no progress', state['status'])

    def test_completion_log_handoff_has_grace_period(self):
        state = {'active': 'minecraft:overworld', 'completedTasks': [], 'paused': False}
        with patch.object(module, 'console', side_effect=['There are 0 of a max of 30 players online:', '[Chunky] No tasks running.']), patch.object(module, 'save'), patch.object(module.time, 'time', return_value=1000), patch.object(Path, 'exists', return_value=False):
            self.assertTrue(module.step(state))
            self.assertEqual(state['status'], 'confirming task completion')
            self.assertEqual(state['completionPendingSince'], 1000)

    def test_final_log_can_exceed_last_saved_checkpoint(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            task = root / 'config/chunky/tasks/minecraft/overworld.properties'
            task.parent.mkdir(parents=True)
            task.write_text('radius=5000\ncenter-x=0\ncenter-z=0\nshape=circle\ncancelled=true\nchunks=393083\n')
            (root / 'logs').mkdir()
            (root / 'logs/latest.log').write_text('Task finished for minecraft:overworld. Processed: 393129 chunks (100.00%)')
            state = {'active': 'minecraft:overworld', 'radius': 5000, 'completed': [], 'completedTasks': [], 'paused': False, 'completionPendingSince': 900, 'lastChunkCount': 388280}
            def reply(command):
                if command == 'list': return 'There are 0 of a max of 30 players online:'
                if command == 'chunky progress': return '[Chunky] No tasks running.'
                if command == 'chunky start': return 'Task started'
                return 'ok'
            with patch.object(module, 'ROOT', root), patch.object(module, 'STOP', root / 'stop'), patch.object(module, 'console', side_effect=reply), patch.object(module, 'save'), patch.object(module.time, 'time', return_value=1000):
                self.assertTrue(module.step(state))
                self.assertIn({'world': 'minecraft:overworld', 'radius': 5000}, state['completedTasks'])
                self.assertEqual(state['active'], 'minecraft:the_nether')


if __name__ == '__main__':
    unittest.main()
