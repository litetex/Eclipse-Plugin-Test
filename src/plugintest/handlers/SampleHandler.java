package plugintest.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import plugintest.Activator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"PluginTest",
				"Hello, Eclipse world");
		
		Thread t = new Thread(() -> this.launch(window.getShell()));
		t.start();
		
		return null;
	}
	
	private void launch(Shell parentShell)
	{
		try
		{
			final ProcessorData pData = new ProcessorData();
			
			Job startJob = Job.create("Starting process...", monitor -> {
				
				monitor.setTaskName("Starting process...");
				
				try {
					ProcessBuilder builder = new ProcessBuilder("ping", "-n", "12" , "localhost");
					Activator.getDefault().getLog().info("Starting '" + String.join(" ", builder.command()) + "'");
					
			        Process p = builder.start();
			        					
					pData.setProcess(p);
				} catch (IOException e) {
					Activator.getDefault().getLog().error("Error while starting", e);
					return new Status(Status.ERROR, getClass(), e.getMessage());
				}
				
				monitor.done();
			
			    return Status.OK_STATUS;
			});
			startJob.schedule();
			
			try
			{
				Thread.sleep(500);
			}
			catch (Exception e) {
				// OH NO
			}
			
			if(pData.getProcess() == null)
			{
				Activator.getDefault().getLog().info("Process null");
				return;
			}
		
		
			if(pData.getProcess().waitFor(30, TimeUnit.SECONDS))
			{
				Activator.getDefault().getLog().info("Everything started");
				Job job = Job.create("Everything started updating ui now", monitor -> {
					
					try
					{
						Thread.sleep(1000);
					}
					catch (Exception e) {
						// OH NO
					}
					
					Activator.getDefault().getLog().info(new BufferedReader(new InputStreamReader(pData.getProcess().getInputStream()))
							  .lines().collect(Collectors.joining("\n")));
					
					Display.getDefault().syncExec(new Runnable() {
					    public void run() {
							MessageDialog.openInformation(
									parentShell,
									"PluginTest",
									"Done " + pData.getProcess().exitValue());
					    }
					});
					
				    return Status.OK_STATUS;
				});
				job.schedule();
			}
			else
			{
				Activator.getDefault().getLog().info("Failed to start in time");
				Job job = Job.create("Failed to start in time...", monitor -> {
					pData.getProcess().destroyForcibly();
					
					Display.getDefault().asyncExec(new Runnable() {
					    public void run() {
							MessageDialog.openInformation(
									parentShell,
									"PluginTest",
									"Killed");
					    }
					});
					
				    return Status.OK_STATUS;
				});
				job.schedule();
			}
		} catch (InterruptedException e) {
			// OH NO
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			Activator.getDefault().getLog().error("ERROR", e);
		}
	}
	
	public class ProcessorData
	{
		private Process process;

		public Process getProcess() {
			return process;
		}

		public void setProcess(Process process) {
			this.process = process;
		}
	}
}
